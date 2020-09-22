package com.hongyan.study.distributedlock.entity;


import com.hongyan.study.distributedlock.enums.OpCode;
import lombok.Data;

@Data
public class R<T> {

    private String msg;

    private Integer code = OpCode.sucess.getCode();

    private T data;

    private static R result;

    public R(Integer code, String msg) {
        this.msg = msg;
        this.code = code;
    }

    public R(Integer code, String msg,  T data) {
        this.msg = msg;
        this.code = code;
        this.data = data;
    }



    public static  R getInstance(){
        if(result == null){
            return new R(OpCode.sucess.getCode(),OpCode.sucess.getValue());
        }
        return result;
    }

}

