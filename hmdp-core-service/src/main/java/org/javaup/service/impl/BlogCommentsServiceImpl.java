package org.javaup.service.impl;

import org.javaup.entity.BlogComments;
import org.javaup.mapper.BlogCommentsMapper;
import org.javaup.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 博客评论服务实现
 * @author: 阿星不是程序员
 **/
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
