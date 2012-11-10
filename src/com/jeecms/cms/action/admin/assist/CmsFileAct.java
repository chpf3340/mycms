package com.jeecms.cms.action.admin.assist;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jeecms.cms.entity.assist.CmsFile;
import com.jeecms.cms.entity.main.CmsSite;
import com.jeecms.cms.manager.assist.CmsFileMng;
import com.jeecms.cms.manager.assist.CmsResourceMng;
import com.jeecms.cms.manager.main.CmsLogMng;
import com.jeecms.cms.web.CmsUtils;
import com.jeecms.cms.web.WebErrors;
import com.jeecms.common.web.RequestUtils;
import com.jeecms.common.web.ResponseUtils;

/**
 * JEECMS附件（包括文章图片多媒体）的Action
 * 
 * @author TOM
 * 
 */
//
@Controller
public class CmsFileAct {
	private static final Logger log = LoggerFactory
			.getLogger(CmsFileAct.class);


	// 直接调用方法需要把root参数保存至model中
	@RequestMapping(value = "/file/v_list.do")
	public String list(HttpServletRequest request, ModelMap model) {
		CmsSite site = CmsUtils.getSite(request);
		String root = (String) model.get("root");
		if (root == null) {
			root = RequestUtils.getQueryParam(request, "root");
		}
		String valid=RequestUtils.getQueryParam(request, "valid");
		Boolean validB=null;
		if(StringUtils.isNotBlank(valid)){
			if(valid.equals("1")){
				validB=true;
			}else{
				validB=false;
			}
		}
		log.debug("list Resource root: {}", root);
		if (StringUtils.isBlank(root)) {
			root = site.getUploadPath();
		}
		String uploadPath = root.substring(site.getUploadPath().length());
		if (uploadPath.length() == 0) {
			uploadPath = "/";
		}
		model.addAttribute("root", root);
		model.addAttribute("rel", uploadPath);
		model.addAttribute("valid",validB);
		model.addAttribute("list", resourceMng.queryFiles(root, validB));
		return "file/list";
	}
	
	@RequestMapping("/file/o_delfreefiles.do")
	public String deleteUnValid(String root,
			HttpServletRequest request, ModelMap model) {
		List<CmsFile>fileList=fileMng.getList(false);
		CmsSite site=CmsUtils.getSite(request);
		String contextPath=site.getContextPath();
		String[]names=new String[fileList.size()];
		String filePath;
		for(int i=0;i<names.length;i++){
			filePath=fileList.get(i).getFilePath();
			if(filePath.contains(site.getContextPath())){
				names[i]=filePath.substring(filePath.indexOf(contextPath)+contextPath.length());
			}
		}
		WebErrors errors = validateDeleteFreeFile(root, names, request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		if(names!=null&&names.length>0){
			int count = resourceMng.delete(names);
			log.info("delete Resource count: {}", count);
			for (String name : names) {
				fileMng.deleteByPath(name);
				log.info("delete Resource name={}", name);
				cmsLogMng.operating(request, "resource.log.delete", "filename="
						+ name);
			}
		}
		model.addAttribute("root", root);
		return list(request, model);
	}

	@RequestMapping("/file/o_delete.do")
	public String delete(String root, String[] names,
			HttpServletRequest request, ModelMap model) {
		WebErrors errors = validateDelete(root, names, request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		int count = resourceMng.delete(names);
		log.info("delete Resource count: {}", count);
		for (String name : names) {
			fileMng.deleteByPath(name);
			log.info("delete Resource name={}", name);
			cmsLogMng.operating(request, "resource.log.delete", "filename="
					+ name);
		}
		model.addAttribute("root", root);
		return list(request, model);
	}

	@RequestMapping("/file/o_delete_single.do")
	public String deleteSingle(HttpServletRequest request, ModelMap model) {
		// TODO 输入验证
		String root = RequestUtils.getQueryParam(request, "root");
		String name = RequestUtils.getQueryParam(request, "name");
		int count = resourceMng.delete(new String[] { name });
		fileMng.deleteByPath(name);
		log.info("delete Resource {}, count {}", name, count);
		cmsLogMng.operating(request, "resource.log.delete", "filename=" + name);
		model.addAttribute("root", root);
		return list(request, model);
	}


	@RequestMapping(value = "/file/v_upload.do")
	public String uploadInput(HttpServletRequest request, ModelMap model) {
		String root = RequestUtils.getQueryParam(request, "root");
		model.addAttribute("root", root);
		return "file/upload";
	}

	@RequestMapping(value = "/file/o_upload.do", method = RequestMethod.POST)
	public String uploadSubmit(String root, HttpServletRequest request,
			ModelMap model) {
		model.addAttribute("root", root);
		return list(request, model);
	}

	@RequestMapping(value = "/file/o_swfupload.do", method = RequestMethod.POST)
	public void swfUpload(
			String root,
			@RequestParam(value = "Filedata", required = false) MultipartFile file,
			HttpServletRequest request, HttpServletResponse response,
			ModelMap model) throws IllegalStateException, IOException {
		resourceMng.saveFile(root, file);
		fileMng.saveFileByPath(root+"//"+file.getOriginalFilename(), file.getOriginalFilename(), false);
		model.addAttribute("root", root);
		log.info("file upload seccess: {}, size:{}.", file
				.getOriginalFilename(), file.getSize());
		ResponseUtils.renderText(response, "");
	}



	private WebErrors validateDelete(String root, String[] names,
			HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		errors.ifEmpty(names, "names");
		for (String id : names) {
			vldExist(id, errors);
		}
		return errors;
	}
	
	private WebErrors validateDeleteFreeFile(String root, String[] names,
			HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		if(names==null||names.length<=0){
			errors.addErrorCode("error.findnofreefile");
		}
		return errors;
	}

	private boolean vldExist(String name, WebErrors errors) {
		if (errors.ifNull(name, "name")) {
			return true;
		}
		return false;
	}

	@Autowired
	private CmsLogMng cmsLogMng;
	@Autowired
	private CmsFileMng fileMng;
	private CmsResourceMng resourceMng;

	@Autowired
	public void setResourceMng(CmsResourceMng resourceMng) {
		this.resourceMng = resourceMng;
	}
}