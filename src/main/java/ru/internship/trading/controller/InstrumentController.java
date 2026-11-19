package ru.internship.trading.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.internship.trading.model.Instrument;
import ru.internship.trading.service.ExchangeService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final ExchangeService exchange;

    public InstrumentController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public Instrument instrument(@Valid @RequestBody Instrument input) {
        return exchange.add(input);
    }

    @GetMapping
    public List<Instrument> instruments() {
        return exchange.instruments();
    }
}