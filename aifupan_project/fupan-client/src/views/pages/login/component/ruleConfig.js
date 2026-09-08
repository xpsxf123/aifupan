export default {
    nickName: [
        {
            required: true,
            message: "昵称不能为空",
            trigger: "blur",
        },
        {
            min: 2,
            max: 20,
            message: "长度在 2 到 20 个字符",
            trigger: "blur",
        }
    ],
    username: [
        {
            required: true,
            message: "用户名不能为空",
            trigger: "blur",
        },
        {
            min: 4,
            max: 20,
            message: "长度在 4 到 20 个字符",
            trigger: "blur",
        }
    ],
    password: [
        {
            required: true,
            message: "密码不能为空",
            trigger: "blur",
        },
        {
            min: 6,
            message: "长度在 6 个字符以上",
            trigger: "blur",
        }
        // {
        //     trigger: 'blur',
        //     validator: (rule, value, callback) => {
        //         var reg =/^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$/
        //         if (reg.test(value)) {
        //             callback();
        //         } else {
        //             callback(new Error('密码格式为6-16位，字母加数字'));
        //         }
        //     }
        // }
    ],
    code: [
        {
            required: true,
            message: "验证码不能为空",
            trigger: "blur",
        }
    ],
    phone: [
        {
            required: true,
            message: "手机号不能为空",
            trigger: "blur",
        }, {
            pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
            message: "请填写正确的手机号码",
            trigger: "blur",
        }
    ],
}