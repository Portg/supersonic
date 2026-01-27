package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class ChatParseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Integer chatId;

    private Integer parseId;

    private Date createTime;

    private String queryText;

    private String userName;

    private String parseInfo;

    private Integer isCandidate;
}
