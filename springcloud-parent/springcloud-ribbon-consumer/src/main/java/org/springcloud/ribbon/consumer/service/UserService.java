/**
 * Project Name:springcloud-ribbon-consumer
 * File Name:HelloService.java
 * Package Name:org.springcloud.ribbon.consumer.service
 * Date:2019年1月9日下午6:55:33
 * Copyright (c) 2019, kaiyun@qk365.com All Rights Reserved.
 *
*/

package org.springcloud.ribbon.consumer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.springcloud.ribbon.consumer.customhystrixcommand.UserCommand;
import org.springcloud.ribbon.consumer.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName:UserService <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Date:     2019年1月9日 下午6:55:33 <br/>
 * @author   kaiyun
 * @version  
 * @since    JDK 1.8
 * @see 	 
 */
@Slf4j
@Service
public class UserService {
	
	@Autowired
	private RestTemplate restTemplate;
	
	
	/**
	 * getUserByIdFallback:服务降级的方法（是Hystrix命令执行失败时使用的后备方法，用来实现服务的降级处理逻辑）. <br/>
	 *
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	public User getUserByIdFallback(Long id) {
		User user = new User();
		user.setId(id);
		user.setUserName("调用出异常啦，启用熔断器");
		return user;
	}
	public List<User> getUserByIdFallback(List<Long> ids) {
		List<User> userList = new ArrayList<>();
		for(Long id : ids) {
			User user = new User();
			user.setId(id);
			user.setUserName("调用出异常啦，启用熔断器");
			userList.add(user);
		}
		return userList;
	}
	
	/**
	 * syncUnAnnotationsgetUserById: 使用自定义请求命令，实现请求的同步执行. <br/>
	 *
	 * @param id
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	public User getUserByIdSyncUnAnnotations(Long id) {
		long start = System.currentTimeMillis();
		
		Setter setter = com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(""));
		UserCommand userCommand = new UserCommand(setter, restTemplate, 1L);
		User user = userCommand.execute();
		
		long end = System.currentTimeMillis();
		log.info("Spend time:{}", (end-start));
		
		return user;
	}
	
	/**
	 * asyncUnAnnotationsgetUserById: 使用自定请求命令，实现请求的异步执行. <br/>
	 *
	 * @param id
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	public Future<User> getUserByIdAsyncUnAnnotations(Long id) {
		long start = System.currentTimeMillis();
		
		Setter setter = com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(""));
		UserCommand userCommand = new UserCommand(setter, restTemplate, 1L);
		Future<User> futrueUser = userCommand.queue();
		
		long end = System.currentTimeMillis();
		log.info("Spend time:{}", (end-start));
		
		return futrueUser;
	}
	
	// @HystrixComand 注解是Hystrix中的核心注解，通过它创建了 HystrixComand 的实现，同时利用 fallback 属性指定了服务降级的实现方法。
	// @HystrixComand 注解实现请求的同步执行，若要实现异步执行则需要另外定义。
	// 通过继承的方式来实现，实现 HystrixCommand 类。即可实现请求的同步执行也可以实现异步执行。（详细见5.3.1）
	
	/**
	 * syncAnnotationsGetUserById: 使用注解@HystrixComand，实现请求的同步执行. <br/>
	 *
	 * @param id
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	@HystrixCommand(fallbackMethod = "getUserByIdFallback")
	public User getUserByIdSyncAnnotations(Long id) {
		long start = System.currentTimeMillis();
		
		User user = restTemplate.getForObject("http://PROVIDER-SERVICE/users/{1}", User.class, id);
		
		long end = System.currentTimeMillis();
		log.info("Spend time:{}", (end-start));
		
		return user; 
	}
	
	/**
	 * asyncAnnotationsGetUserByIdAsync: 使用注解@HystrixComand，实现请求的异步处理. <br/>
	 *
	 * @param id
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	@HystrixCommand(fallbackMethod = "getUserByIdFallback")
	public Future<User> getUserByIdAsyncAnnotations(final Long id) {
		long start = System.currentTimeMillis();
		
		Future<User> futrueUser = new AsyncResult<User>() {
			@Override
			public User invoke() {
				return restTemplate.getForObject("http://PROVIDER-SERVICE/users/{1}", User.class, id);
			}
		};
		
		long end = System.currentTimeMillis();
		log.info("Spend time:{}", (end-start));
		
		return futrueUser;
	}
	
	
// =================================== 以下是 服务容错保护中，请求合并，方法，通过继承类实现方式，见UserCollapseCommand类=========================================	
	
	// @HystrixCollapser 实现了在 /users/{id} 依赖服务之前设置了一个批量请求合并器
	@HystrixCollapser(batchMethod="findAll", collapserProperties= {@HystrixProperty(name="timerDelayInMilliseconds", value="100")} )
	public User find(Long id) {
		return null; 
	}
	
	/**
	 * getAllUser: 获取所有的用户. <br/>
	 *
	 * @param id
	 * @return
	 * @since JDK 1.8
	 * @author kaiyun
	 */
	@HystrixCommand(fallbackMethod = "getUserByIdFallback")
	public List<User> findAll(List<Long> ids) {
		long start = System.currentTimeMillis();
		
		List<User> userList = restTemplate.getForObject("http://PROVIDER-SERVICE/users?ids={1}", List.class, StringUtils.join(ids, ","));
		
		long end = System.currentTimeMillis();
		log.info("Spend time:{}", (end-start));
		
		return userList; 
	}
	
}
