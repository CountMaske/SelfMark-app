import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:view/stores/TokenManager.dart';
import 'package:view/stores/UserController.dart';
import 'package:view/viewmodels/user.dart';

class MineView extends StatefulWidget {
  const MineView({super.key});

  @override
  _MineViewState createState() => _MineViewState();
}

class _MineViewState extends State<MineView> {
  // 从全局拿用户控制器（MainPage里已经Get.put注册过了）
  final UserController _userController = Get.find<UserController>();

  // 返回退出登录的元素：已登录才显示"退出"
  Widget _getLogout() {
    return _userController.user.value.id.isNotEmpty
        ? GestureDetector(
            onTap: () {
              // 弹出确认提示框
              showDialog(
                context: context,
                builder: (context) {
                  return AlertDialog(
                    title: Text("提示"),
                    content: Text("确认退出登录吗"),
                    actions: [
                      TextButton(
                        onPressed: () {
                          Navigator.pop(context);
                        },
                        child: Text("取消"),
                      ),
                      TextButton(
                        onPressed: () async {
                          // 清除Getx 删除token
                          await tokenManager.removeToken();
                          // Getx内存数据
                          _userController.updateUserInfo(UserInfo.fromJSON({}));
                          Navigator.pop(context);
                        },
                        child: Text("确认"),
                      ),
                    ],
                  );
                },
              );
            },
            child: Text("退出", style: TextStyle(color: Colors.grey)),
          )
        : Text("");
  }

  // 隐藏手机号中间4位，保护隐私（如 138****1234）
  String _maskAccount(String account) {
    if (account.length == 11) {
      // substring(0, 3) → 取索引 0、1、2，即前 3 位
      // substring(7)    → 从索引 7 取到末尾，即后 4 位
      return '${account.substring(0, 3)}****${account.substring(7)}';
    }
    // 非标准长度：保留首尾各1位，中间用*代替
    // substring(0, 1)                   → 取索引 0，即第 1 位
    // '*' * (account.length - 2)        → 中间位数，用 * 重复拼接
    // substring(account.length - 1)     → 从倒数第 1 位取到末尾，即最后 1 位
    if (account.length > 2) {
      return '${account.substring(0, 1)}${'*' * (account.length - 2)}${account.substring(account.length - 1)}';
    }
    return account;
  }

  Widget _buildHeader() {
    return Container(
      // color: Colors.white,
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          CircleAvatar(
            radius: 40,
            backgroundImage: AssetImage('lib/assets/mine/avatar.png'),
          ),
          SizedBox(width: 12),
          // 让 Column 撑满 Row 里的剩余空间，把退出按钮顶到最右，同时防止昵称过长溢出
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start, // 靠左对齐
              children: [
                // Obx监听响应式数据：登录/退出时昵称自动刷新
                Obx(() {
                  return GestureDetector(
                    onTap: () {
                      // 没有用户信息的时候可以去登录
                      if (_userController.user.value.id.isEmpty) {
                        Navigator.pushNamed(context, '/login');
                      }
                    },
                    child: Text(
                      _userController.user.value.username.isNotEmpty
                          ? _userController
                                .user
                                .value
                                .username // 已登录显示昵称
                          : '立即登录', // 未登录显示立即登录
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  );
                }),
                SizedBox(height: 6),
                // Obx监听响应式数据：登录后手机号自动刷新（已脱敏显示）
                Obx(() => Text(
                  _userController.user.value.account.isNotEmpty
                      ? _maskAccount(_userController.user.value.account)
                      : '',
                )),
              ],
            ),
          ),
          // 已登录时右侧显示退出按钮
          Obx(() => _getLogout()),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        // AppBar 右侧按钮区（水平排布）
        actions: [
          // // 右上角「扫一扫」入口
          IconButton(icon: Icon(Icons.qr_code_scanner), onPressed: () {}),
          SizedBox(width: 12),
        ],
      ),
      body: SingleChildScrollView(child: Column(children: [_buildHeader()])),
    );
  }
}
