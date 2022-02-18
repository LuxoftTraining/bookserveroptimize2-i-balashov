package com.luxoft.highperformance.bookserver;

import com.luxoft.highperformance.bookserver.measure.Measure;
import com.luxoft.highperformance.bookserver.model.Book;
import com.luxoft.highperformance.bookserver.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("books")
public class BookController {

    public final int BOOKS_AMOUNT=10_000;

    @Autowired
    BookRepository bookRepository;

    @Measure(value = "baseline", warmup = 50)
    @GetMapping("keywords0/{keywordsString}")
    public List<Book> getBookByTitle(@PathVariable String keywordsString) {
        String[] keywords = keywordsString.split(" ");
        if (keywords.length == 1) {
            return bookRepository.findAllByTitleContaining(keywords[0]);
        } else if (keywords.length == 2) {
            return bookRepository.findAllByTitleContainingAndTitleContaining(
                keywords[0], keywords[1]);
        } else if (keywords.length == 3) {
            return bookRepository.findAllByTitleContainingAndTitleContainingAndTitleContaining(
                keywords[0], keywords[1], keywords[2]);
        }
        return null;
    }

    @Measure(value = "3 keywords indexed in DB", warmup = 50)
    @GetMapping("keywords1/{keywordsString}")
    @Transactional(readOnly = true)
    public List<Book> getBookByTitleDB(@PathVariable String keywordsString) {
        String[] keywords = keywordsString.split(" ");
        if (keywords.length == 1) {
            List<Book> list1 = bookRepository.findByKeyword1(keywords[0]);
            List<Book> list2 = bookRepository.findByKeyword2(keywords[0]);
            List<Book> list3 = bookRepository.findByKeyword3(keywords[0]);
            list1.addAll(list2);
            list1.addAll(list3);
            return list1;
        } else if (keywords.length == 2) {
            List<Book> list1 = bookRepository.findByKeyword1AndKeyword2(
                    keywords[0], keywords[1]);
            List<Book> list2 = bookRepository.findByKeyword1AndKeyword2(
                    keywords[1], keywords[0]);
            List<Book> list3 = bookRepository.findByKeyword2AndKeyword3(
                    keywords[0], keywords[1]);
            List<Book> list4 = bookRepository.findByKeyword2AndKeyword3(
                    keywords[1], keywords[0]);
            List<Book> list5 = bookRepository.findByKeyword1AndKeyword3(
                    keywords[0], keywords[1]);
            List<Book> list6 = bookRepository.findByKeyword1AndKeyword3(
                    keywords[1], keywords[0]);
            list1.addAll(list2);
            list1.addAll(list3);
            list1.addAll(list4);
            list1.addAll(list5);
            list1.addAll(list6);
            return list1;
        } else if (keywords.length == 3) {
            List<Book> list1 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[0], keywords[1], keywords[2]);
            List<Book> list2 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[0], keywords[2], keywords[1]);
            List<Book> list3 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[1], keywords[0], keywords[2]);
            List<Book> list4 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[1], keywords[2], keywords[0]);
            List<Book> list5 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[2], keywords[1], keywords[0]);
            List<Book> list6 = bookRepository.findByKeyword1AndKeyword2AndKeyword3(
                    keywords[2], keywords[0], keywords[1]);
            list1.addAll(list2);
            list1.addAll(list3);
            list1.addAll(list4);
            list1.addAll(list5);
            list1.addAll(list6);
            return list1;
        }
        return null;
    }

    @Autowired
    DataSource dataSource;

    @Measure(value = "JDBC", warmup = 50)
    @GetMapping("keywords2/{keywordsString}")
    @Transactional
    public List<Book> getBookByTitleJDBC(@PathVariable String keywordsString) throws SQLException {
        StringBuilder builder = new StringBuilder();
        String[] keywords = keywordsString.split(" ");
        builder.append("'").append(keywords[0]).append("'");
        if (keywords.length>1) {
            builder.append(",'").append(keywords[1]).append("'");
        }
        if (keywords.length>2) {
            builder.append(",'").append(keywords[2]).append("'");
        }
        String keywordsIn = builder.toString();

        List<Book> books = new ArrayList<>();
        String SQL = "SELECT * FROM BOOK ";
        if (keywords.length == 1) {
            SQL += "WHERE KEYWORD1='"+keywords[0]+"' OR " +
                    "KEYWORD2='"+keywords[0]+"' OR " +
                    "KEYWORD3='"+keywords[0]+"'";
        } else if (keywords.length == 2) {
            SQL += "WHERE "+
                    "(KEYWORD1='"+keywords[0]+"' AND KEYWORD2='"+keywords[1]+"') OR " +
                    "(KEYWORD2='"+keywords[0]+"' AND KEYWORD1='"+keywords[1]+"') OR "+
                    "(KEYWORD1='"+keywords[0]+"' AND KEYWORD3='"+keywords[1]+"') OR " +
                    "(KEYWORD3='"+keywords[0]+"' AND KEYWORD1='"+keywords[1]+"') OR "+
                    "(KEYWORD2='"+keywords[0]+"' AND KEYWORD3='"+keywords[1]+"') OR " +
                    "(KEYWORD3='"+keywords[0]+"' AND KEYWORD2='"+keywords[1]+"')";
        } else if (keywords.length == 3) {
            SQL += "WHERE (KEYWORD1 IN (" + keywordsIn + ")) ";
            SQL += "AND (KEYWORD2 IN (" + keywordsIn + ")) ";
            SQL += "AND (KEYWORD3 IN (" + keywordsIn + ")) ";
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection
                     .prepareStatement(SQL)) {
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) return books; // nothing found
            Book book = new Book();
            book.setId(resultSet.getInt("ID"));
            book.setTitle(resultSet.getString("TITLE"));
            book.setKeyword1(resultSet.getString("KEYWORD1"));
            book.setKeyword2(resultSet.getString("KEYWORD2"));
            book.setKeyword3(resultSet.getString("KEYWORD3"));
            books.add(book);
        }

        return books;
    }

    @Measure("test")
    @GetMapping("test")
    public String getTest() {
        return "just a test!";
    }

    @GetMapping("/random")
    public Book getBookRandom() {
        Random random = new Random();
        int index = random.nextInt(BOOKS_AMOUNT);
        List<Book> all = bookRepository.findAll();
        return all.get(index);
    }

    @Measure(value = "baseline", warmup = 50)
    @GetMapping("keywords3/{keywordsString}")
    public Set<Book> getBookByTitleHashMap(@PathVariable String keywordsString) {
        String[] keywords = keywordsString.split(" ");

        Set<Book> bookSet = null;
        for (String keyword : keywords) {
            Set<Book> booksWithKeywordSet = new HashSet<>();
            Map<String, Set<Book>> map = Book.keywordMap;
            if (map.containsKey(keyword)) {
                booksWithKeywordSet.addAll(map.get(keyword));
            }
            if (bookSet == null) {
                bookSet = booksWithKeywordSet;
            } else {
                bookSet.retainAll(booksWithKeywordSet);
            }
        }

        return bookSet;
    }

    @GetMapping
    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("read-all")
    public void readAll() {
        List<Book> all = bookRepository.findAll();
        for (Book book: all) {
            Book.initKeywords(book);
        }
    }

    @PostMapping
    public Book addBook(@RequestBody Book book) {
        Book.initKeywords(book);
        return bookRepository.save(book);
    }

}
