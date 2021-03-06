package cn.intmsaze.project.web;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.intsmaze.common.util.Result;
import org.intsmaze.common.util.ResultGenerator;
import cn.intmsaze.project.model.Person;
import cn.intmsaze.project.service.DubboService;
import cn.intmsaze.project.service.PersonService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* Created by CodeGenerator on 2018/03/30.
*/
@RestController
@RequestMapping("/person")
//@CrossOrigin
public class PersonController {
	
	
    @Resource
    private PersonService personService;
    
    @Resource
    DubboService dubboService;
    
    @RequestMapping("/get/dubbo/{id}")
    public Result dubboRpc(HttpServletResponse response,@PathVariable Integer id) {
    	Person person = dubboService.findById(id);
        return ResultGenerator.genSuccessResult(person);
    }
    
    @PostMapping
    public Result add(@RequestBody Person person) {
        personService.save(person);
        return ResultGenerator.genSuccessResult();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        personService.deleteById(id);
        return ResultGenerator.genSuccessResult();
    }

    @PutMapping
    public Result update(@RequestBody Person person) {
        personService.update(person);
        return ResultGenerator.genSuccessResult();
    }

    @RequestMapping("/get/{id}")
    public Result detail(HttpServletResponse response,@PathVariable Integer id) {
//    	response.addHeader("Access-Control-Allow-Origin", "*");
    	//或者添加@CrossOrigin注解即可解决跨域问题或在WebMvcConfigurer类中使用addCorsMappings()
        Person person = personService.findById(id);
        return ResultGenerator.genSuccessResult(person);
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "0") Integer size) {
        PageHelper.startPage(page, size);
        List<Person> list = personService.findAll();
        PageInfo pageInfo = new PageInfo(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }
}

