package com.hongyan.study.distributedlock.enums;

public enum OpCode {
    /**
     * 成功
     */
    sucess(200,"ok"),
    /***
     * 内部异常
     */
    Internal(601,"内部异常"),

    /***
     * 无效参数异常
     */
    InvalidArgument(602,"无效参数异常"),
    /***
     * 无效对象
     */
    InvalidObject(630,"无效对象"),
    ;

    private Integer code;
    private String value;

    OpCode(Integer code, String value) {
        this.code = code;
        this.value = value;
    }


    public static String getValueByCode(Integer code){
        OpCode[] values =  values();
        for (OpCode opCode : values){
            if(opCode.code.intValue() == code){
                return opCode.value;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
