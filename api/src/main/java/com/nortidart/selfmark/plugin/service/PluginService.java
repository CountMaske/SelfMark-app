package com.nortidart.selfmark.plugin.service;

import com.nortidart.selfmark.plugin.dto.MarketPluginResponse;
import com.nortidart.selfmark.plugin.mapper.PluginMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PluginService {
    private final PluginMapper pluginMapper;

    public PluginService(PluginMapper pluginMapper) {
        this.pluginMapper = pluginMapper;
    }

    public List<MarketPluginResponse> listMarket(Long userId) {
        return pluginMapper.selectMarket(userId);
    }
}
