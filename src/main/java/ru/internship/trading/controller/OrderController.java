package ru.internship.trading.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.internship.trading.dto.Amendment;
import ru.internship.trading.dto.NewOrder;
import ru.internship.trading.enums.Status;
import ru.internship.trading.model.Order;
import ru.internship.trading.service.ExchangeService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final ExchangeService exchange;

    public OrderController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public Order place(@RequestHeader("X-User-Id") String user, @Valid @RequestBody NewOrder input) {
        return exchange.place(user, input);
    }

    @PutMapping("/{id}")
    public Order amend(@PathVariable long id, @RequestHeader("X-User-Id") String user,
                       @Valid @RequestBody Amendment input) {
        return exchange.amend(user, id, input);
    }

    @DeleteMapping("/{id}")
    public Order cancel(@PathVariable long id, @RequestHeader("X-User-Id") String user) {
        return exchange.cancel(user, id);
    }

    @GetMapping
    public List<Order> orders(@RequestParam(required = false) String seccode,
                              @RequestParam(required = false) Status status,
                              @RequestParam(name = "account_id", required = false) Long accountId) {
        return exchange.orders(seccode, status, accountId);
    }
}