package sp.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/up")
public class ServerInfoController {

	public static String getIp() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return "unknown";
		}
		HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
		return request.getRemoteAddr();
	}

	@GetMapping("/ip")
	public String ip() {
		return getIp();
	}

}