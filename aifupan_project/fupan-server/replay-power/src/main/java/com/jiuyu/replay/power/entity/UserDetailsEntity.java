package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.replay.common.baseEntity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户详情表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-21 15:26:13
 */
@Data
@TableName("tb_user_details")
public class UserDetailsEntity extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * tb_user的id
	 */
	private Long userId;
	/**
	 * 行业id
	 */
	private Long tradeId;
	/**
	 * 公司表id
	 */
	private Long companyId;
	/**
	 * 类型  0个人主播   1公司
	 */
	private Integer anchorType;
	/**
	 * 公司所在职位
	 */
	private String position;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 身份证号
	 */
	private String idCard;
	/**
	 * 真实名称
	 */
	private String realName;
	/**
	 * 性别 1男 2女
	 */
	private Integer sex;
	/**
	 * 出生年月日
	 */
	private Date birthday;
	/**
	 * 现用户地址
	 */
	private String address;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 是否删除（0未删除 1删除）
	 */
	private Integer isDeleted;

	/**
	 * 用户来源渠道ID
	 */
	@Schema(description = "用户来源渠道ID")
	private Long channelId;

	/**
	 * 用户跟进销售人员ID
	 */
	@Schema(description = "用户跟进销售人员ID")
	private Long saleId;

	/**
	 * 用户微信名称
	 */
	@Schema(description = "用户微信名称")
	private String wxName;

	/**
	 * 代理商销售ID
	 */
	private Long agentSaleId;
	/**
	 * 是否演示
	 */
	private Integer isShow;

	/**
	 * 视频会议地址
	 */
	private String videoMeetPath;
	/**
	 * 用户意向
	 */
	@Schema(description = "用户意向")
	private String userAmbition;
	/**
	 * 客户类型0个人、1工作室、2企业
	 */
	@Schema(description = "客户类型0个人、1工作室、2企业")
	private Integer userBelongType;
	/**
	 * 是否登陆过 0未 1已登录过
	 */
	private Integer isLoggedIn;

    /**
     * 代理商id
     */
    private Long agentId;

    /**
     * 员工状态 0离职  1在职
     */
    private Integer employeeStatus;

    /**
     * 是否提交预约 0-否 1-是
     */
    private Integer isSubmitAppointment;

}
