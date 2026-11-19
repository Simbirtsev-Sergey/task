package ru.internship.trading.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.internship.trading.model.Trade;
import ru.internship.trading.service.ExchangeService;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final ExchangeService exchange;

    public TradeController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @GetMapping
    public List<Trade> trades(@RequestParam(required = false) String seccode) {
        return exchange.trades(seccode);
    }
}