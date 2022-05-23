package noob.toolbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import noob.toolbox.domain.dto.DomainDto;
import noob.toolbox.domain.entity.Domain;
import noob.toolbox.domain.entity.Groups;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.domain.vo.GroupsVo;
import noob.toolbox.service.DomainService;
import noob.toolbox.service.GroupsService;
import noob.toolbox.service.IndexService;
import noob.toolbox.validated.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("invitation")
public class InvitationController {

    @Autowired
    private DomainService domainService;

    @Autowired
    private GroupsService groupsService;

    @Autowired
    private IndexService indexService;

    @GetMapping
    public Page<Domain> query(DomainDto domainDto) {
        return domainService.query(domainDto);
    }

    @PostMapping
    public ResultRow create(
            @RequestBody
            Domain domain) {
        if (ObjectUtils.isEmpty(domain.getName())) return ResultRow.fail("域名不能为空");
        if (!domain.getName().matches("^\\w+\\.\\w+$"))  return ResultRow.fail("域名输入有误");
        return domainService.create(domain);
    }

    @PatchMapping
    public ResultData update(
            @RequestBody
            @Validated(Update.class)
            Domain domain) {
        domainService.updateByPrimaryKeySelective(domain);
        return ResultData.success();
    }

    @DeleteMapping
    public ResultData delete(
            @RequestBody
            @NotEmpty(message = "数组不能为空")
            List<Integer> ids) {
        domainService.deleteByIds(ids);
        return ResultData.success();
    }

    @GetMapping("group")
    public List<GroupsVo> queryGroup(Groups groups) {
        return groupsService.queryGroupsSelective(groups);
    }

    @PostMapping("group")
    public ResultData createGroup(
            @RequestBody
            @Valid
            Groups groups) {
        groupsService.create(groups);
        return ResultData.success();
    }

    @PatchMapping("group")
    public ResultData updateGroup(
            @RequestBody
            @Validated(Update.class)
            Groups groups) {
        groupsService.updateByPrimaryKeySelective(groups);
        return ResultData.success();
    }

    @DeleteMapping("group/{id}")
    public ResultData deleteGroup(
            @PathVariable
            Integer id) {
        groupsService.deleteById(id);
        return ResultData.success();
    }

    @PostMapping("page")
    public ResultData createPage(
            @NotNull(message = "站点文件不能为空")
            MultipartFile file,
            @NotBlank(message = "站点名不能为空")
            @Pattern(regexp = "^[a-z0-9]$", message = "站点名只能是a-z/0-9")
            String name) throws IOException {
        indexService.create(file, name);
        return ResultData.success();
    }

    @GetMapping("page")
    public List<String> listPage() {
        return indexService.listAll();
    }

    @PatchMapping("page")
    public ResultData updatePage(
            @NotBlank(message = "旧站点名不能为空")
            String oldName,
            @NotBlank(message = "新站点名不能为空")
            String newName) {
        indexService.update(oldName, newName);
        return ResultData.success();
    }

    @DeleteMapping("page/{pageName}")
    public ResultData deletePage(
            @PathVariable
            @NotBlank(message = "站点名不能为空")
            String pageName) throws IOException {
        indexService.delete(pageName);
        return ResultData.success();
    }

}
