package ru.internship.trading.model;

import java.util.List;

public record Quotes(String seccode, List<Level> bids, List<Level> asks) {}