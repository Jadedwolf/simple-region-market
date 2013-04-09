package me.ienze.SimpleRegionMarket.signs;

import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;

/**
 * @author theZorro266
 * 
 */
public class TemplateSell extends TemplateMain {
	public TemplateSell(SimpleRegionMarket plugin, TokenManager tokenManager, String tplId) {
		super(plugin, tokenManager);
		id = tplId;
		load();
	}
}
