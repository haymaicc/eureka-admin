package cn.springcloud.eureka.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;

import cn.springcloud.eureka.ResultMap;
import cn.springcloud.eureka.http.HttpUtil;

@RestController
@RequestMapping("eureka")
public class EurekaClientController {

	@Resource
	private EurekaClient eurekaClient;

	@Value("${eureka.client.serviceUrl.defaultZone}")
	private String defaultZone;

	/**
	 * @description 获取服务数量和节点数量
	 */
	@RequestMapping(value = "home", method = RequestMethod.GET)
	public ResultMap home() {
		List<Application> apps = eurekaClient.getApplications().getRegisteredApplications();
		int appCount = apps.size();
		int nodeCount = 0;
		int enableNodeCount = 0;
		for (Application app : apps) {
			nodeCount += app.getInstancesAsIsFromEureka().size();
			List<InstanceInfo> instances = app.getInstances();
			for (InstanceInfo instance : instances) {
				if (instance.getStatus().name().equals(InstanceStatus.UP.name())) {
					enableNodeCount++;
				}
			}
		}
		return ResultMap.buildSuccess().put("appCount", appCount).put("nodeCount", nodeCount).put("enableNodeCount",
				enableNodeCount);
	}

	/**
	 * @description 获取所有服务节点
	 */
	@RequestMapping(value = "apps", method = RequestMethod.GET)
	public ResultMap apps() {
		List<Application> apps = eurekaClient.getApplications().getRegisteredApplications();
		Collections.sort(apps, Comparator.comparing(Application::getName));
		for (Application app : apps) {
			Collections.sort(app.getInstances(), Comparator.comparingInt(InstanceInfo::getPort));
		}
		return ResultMap.buildSuccess().put("list", apps);
	}

	/**
	 * @description 界面请求转到第三方服务进行状态变更
	 */
	@RequestMapping(value = "status/{appName}", method = RequestMethod.POST)
	public ResultMap status(@PathVariable String appName, String instanceId, String status) {
		// 拼凑url
		String outOfServiceUrl = defaultZone + "apps/%s/%s/status?value=OUT_OF_SERVICE";
		String upUrl = defaultZone + "apps/%s/%s/status?value=UP";

		if (status.equalsIgnoreCase("UP")) {
			upUrl = String.format(upUrl, appName, instanceId);
			HttpUtil.delete(upUrl);
		} else if (status.equalsIgnoreCase("OUT_OF_SERVICE")) {
			outOfServiceUrl = String.format(outOfServiceUrl, appName, instanceId);
			HttpUtil.put(outOfServiceUrl);
		}
		return ResultMap.buildSuccess();
	}
}
