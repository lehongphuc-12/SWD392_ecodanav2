package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.InappropriateWord;
import com.ecodana.evodanavn1.repository.InappropriateWordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InappropriateWordService {

    @Autowired
    private InappropriateWordRepository inappropriateWordRepository;

    private final AtomicReference<Set<String>> cachedActiveWords = new AtomicReference<>(Set.of());

    public List<InappropriateWord> getActiveWords() {
        return inappropriateWordRepository.findByIsActiveTrueOrderByWordAsc();
    }

    public void refreshCache() {
        Set<String> words = getActiveWords().stream()
                .map(InappropriateWord::getWord)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
        cachedActiveWords.set(Collections.unmodifiableSet(words));
    }

    public Set<String> findMatches(String content) {
        if (content == null || content.isBlank()) return Set.of();
        if (cachedActiveWords.get().isEmpty()) {
            refreshCache();
        }

        String normalized = normalize(content);
        // Tách từ đơn giản theo ký tự không phải chữ/số (bao gồm tiếng Việt sau khi remove dấu)
        Set<String> tokens = Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Set<String> active = cachedActiveWords.get();

        // Khớp theo từ nguyên vẹn, và thêm kiểm tra substring an toàn (tùy chọn)
        Set<String> matches = new TreeSet<>();
        for (String word : active) {
            if (tokens.contains(word)) {
                matches.add(word);
            } else if (word.length() >= 4 && normalized.contains(word)) { // tránh false positive cho từ rất ngắn
                matches.add(word);
            }
        }
        return matches;
    }

    private String normalize(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        // Remove diacritics
        String withoutDiacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(decomposed).replaceAll("");
        // Normalize spaces
        return withoutDiacritics.trim();
    }
}


