package ru.internship.trading.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.internship.trading.dto.OpenAccount;
import ru.internship.trading.model.Account;
import ru.internship.trading.service.ExchangeService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final ExchangeService exchange;

    public AccountController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public Account account(@Valid @RequestBody OpenAccount input) {
        return exchange.open(input);
    }

    @GetMapping("/{id}")
    public Account account(@PathVariable long id, @RequestHeader("X-User-Id") String user) {
        return exchange.account(id, user);
    }
}