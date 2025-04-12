package com.bookstore.api.utils;

import com.bookstore.api.models.Book;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final String CACHE_FILE = "book_cache.dat";

    // Load cache from file
    public static Map<Integer, Book> loadCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) {
            return new ConcurrentHashMap<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CACHE_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                return (Map<Integer, Book>) obj;
            } else {
                System.err.println("Invalid cache format. Resetting...");
                file.delete(); // Delete corrupt cache
                return new ConcurrentHashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading cache, resetting: " + e.getMessage());
            file.delete(); // Delete corrupt cache
            return new ConcurrentHashMap<>();
        }
    }


    // Save cache to file
    public static void saveCache(Map<Integer, Book> bookCache) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CACHE_FILE))) {
            oos.writeObject(bookCache);
        } catch (IOException e) {
            System.err.println("Error saving cache: " + e.getMessage());
        }
    }
}
