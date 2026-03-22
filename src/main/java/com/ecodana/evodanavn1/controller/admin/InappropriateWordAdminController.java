package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.InappropriateWord;
import com.ecodana.evodanavn1.repository.InappropriateWordRepository;
import com.ecodana.evodanavn1.service.InappropriateWordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/inappropriate-words")
public class InappropriateWordAdminController {

    @Autowired
    private InappropriateWordRepository inappropriateWordRepository;

    @Autowired
    private InappropriateWordService inappropriateWordService;

    @GetMapping
    public String list(Model model, HttpSession session) {
        List<InappropriateWord> words = inappropriateWordRepository.findAll();
        model.addAttribute("words", words);
        return "admin/admin-inappropriate-words";
    }

    @PostMapping("/add")
    public String add(@RequestParam String word,
                      @RequestParam(required = false) String category,
                      @RequestParam(required = false, defaultValue = "MEDIUM") InappropriateWord.Severity severity) {
        if (word != null && !word.isBlank() && !inappropriateWordRepository.existsByWordIgnoreCase(word)) {
            InappropriateWord w = new InappropriateWord();
            w.setId(UUID.randomUUID().toString());
            w.setWord(word.trim());
            w.setCategory(category);
            w.setSeverity(severity);
            w.setActive(true);
            inappropriateWordRepository.save(w);
            inappropriateWordService.refreshCache();
        }
        return redirectToDashboard();
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam String id,
                         @RequestParam boolean active) {
        inappropriateWordRepository.findById(id).ifPresent(w -> {
            w.setActive(active);
            inappropriateWordRepository.save(w);
            inappropriateWordService.refreshCache();
        });
        return redirectToDashboard();
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String id) {
        inappropriateWordRepository.deleteById(id);
        inappropriateWordService.refreshCache();
        return redirectToDashboard();
    }

    private String redirectToDashboard() {
        return "redirect:/admin/dashboard?tab=inappropriateWords";
    }
}


