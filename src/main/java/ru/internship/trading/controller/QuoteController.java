package ru.internship.trading.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.internship.trading.model.Quotes;
import ru.internship.trading.service.ExchangeService;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {
    private final ExchangeService exchange;

    public QuoteController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @GetMapping("/{seccode}")
    public Quotes quotes(@PathVariable String seccode) {
        return exchange.quotes(seccode);
    }
}