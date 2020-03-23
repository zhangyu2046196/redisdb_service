package com.longbei.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author zhangy
 * @version 1.0
 * @description
 * @date 2020/3/18 11:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student implements Serializable {
    private static final long serialVersionUID = -6374079249064166679L;

    private Integer id;

    private String name;

    private Integer age;
}
