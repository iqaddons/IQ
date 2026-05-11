package net.iqaddons.mod.screen.model;

import java.util.List;

public record ConfigCategory(String name, String id, List<ConfigEntryModel> entries) {
}