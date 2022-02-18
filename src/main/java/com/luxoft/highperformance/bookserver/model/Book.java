package com.luxoft.highperformance.bookserver.model;


import lombok.*;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Getter @Setter @ToString
@Table(indexes = {
        @Index(columnList = "KEYWORD1"),
        @Index(columnList = "KEYWORD2"),
        @Index(columnList = "KEYWORD3")
})
public class Book {
    @Id
    @GeneratedValue
    private Integer id;
    private String title;
    private String keyword1;
    private String keyword2;
    private String keyword3;

    public final static int KEYWORDS_AMOUNT = 3;
    public static Map<String, Set<Book>> keywordMap = new ConcurrentHashMap<>();

    public static void initKeywords(Book book) {
        String[] keywords = book.getTitle().split(" ");
        if (keywords.length > 0) book.setKeyword1(keywords[0]);
        if (keywords.length > 1) book.setKeyword2(keywords[2]);
        if (keywords.length > 2) book.setKeyword3(keywords[3]);
        addToHashMaps(book, List.of(keywords[0],keywords[2],keywords[3]));
    }

    private static void addToHashMaps(Book book, List<String> keywords) {
        for (int i=0; i< KEYWORDS_AMOUNT; i++) {
            String keyword = keywords.get(i);
            if (keywordMap.containsKey(keyword)) {
                keywordMap.get(keyword).add(book);
            } else {
                HashSet<Book> set = new HashSet<>();
                set.add(book);
                keywordMap.put(keyword, set);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Book book = (Book) o;

        return title != null ? title.equals(book.title) : book.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}
