package com.example.chat_agent.controller;

import com.example.chat_agent.entity.Conversation;
import com.example.chat_agent.service.ChatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class ChatController {

    private final ChatService chatService;
    private static final ThreadLocal<String> USER_SESSION = new ThreadLocal<>();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String chatView(Model model) {
        String userId = USER_SESSION.get();
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            USER_SESSION.set(userId);
        }
        model.addAttribute("userId", userId);
        return "chat";
    }

    @PostMapping("/chat/send")
    @ResponseBody
    public String sendMessage(@RequestParam("userId") String userId,
                              @RequestParam("message") String message) {
        return chatService.sendMessage(userId, message);
    }

    @GetMapping("/chat/history")
    @ResponseBody
    public List<Conversation> getHistory(@RequestParam("userId") String userId) {
        return chatService.getHistory(userId);
    }
}
