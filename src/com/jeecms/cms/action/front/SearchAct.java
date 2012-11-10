package com.jeecms.cms.action.front;

import static com.jeecms.cms.Constants.TPLDIR_SPECIAL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jeecms.cms.entity.main.CmsSite;
import com.jeecms.cms.web.CmsUtils;
import com.jeecms.cms.web.FrontUtils;
import com.jeecms.common.web.RequestUtils;

@Controller
public class SearchAct {
	public static final String SEARCH_INPUT = "tpl.searchInput";
	public static final String SEARCH_RESULT = "tpl.searchResult";
	public static final String SEARCH_ERROR = "tpl.searchError";

	@RequestMapping(value = "/search*.jspx", method = RequestMethod.GET)
	public String index(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		CmsSite site = CmsUtils.getSite(request);
		// 将request中所有参数保存至model中。
		model.putAll(RequestUtils.getQueryParams(request));
		FrontUtils.frontData(request, model, site);
		FrontUtils.frontPageData(request, model);
		String q = RequestUtils.getQueryParam(request, "q");
		if(q.equals("?")||q.equals("*")){
			return FrontUtils.getTplPath(request, site.getSolutionPath(),
					TPLDIR_SPECIAL, SEARCH_ERROR);
		}
		if(q.startsWith("?")||q.startsWith("*")){
			model.addAttribute("oldq",q);
			q=q.substring(1);
			//替换关键词
			model.addAttribute("q",q);
		}
		String channelId = RequestUtils.getQueryParam(request, "channelId");
		if (StringUtils.isBlank(q) && StringUtils.isBlank(channelId)) {
			model.remove("q");
			model.remove("channelId");
			return FrontUtils.getTplPath(request, site.getSolutionPath(),
					TPLDIR_SPECIAL, SEARCH_INPUT);
		} else {
			return FrontUtils.getTplPath(request, site.getSolutionPath(),
					TPLDIR_SPECIAL, SEARCH_RESULT);
		}
	}
}
