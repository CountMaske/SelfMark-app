import 'package:flutter/material.dart';

/// 轻提示工具类。
///
/// 基于 [SnackBar] 实现类 Toast 效果，并做了防抖：
/// 3 秒内重复调用只显示一次，避免连续弹提示刷屏。
class ToastUtils {
  // ===== 防抖阀门 =====
  // true 表示"当前已有一条提示正在展示"，期间新的提示会被忽略。
  // 用 static 是因为工具类只提供静态方法，没有实例，状态得挂在类上。
  static bool showLoading = false;

  /// 显示一条轻提示。
  ///
  /// [msg] 为空时兜底显示 "加载成功"。
  static void showToast(BuildContext context, String? msg) {
    // 阀门已开：说明上一条提示还没消失，直接丢弃本次调用（防抖核心）
    if (ToastUtils.showLoading) {
      return;
    }

    // 上锁：标记"正在展示中"
    ToastUtils.showLoading = true;

    // 3 秒后解锁，与下方 SnackBar 的 duration 保持一致
    Future.delayed(Duration(seconds: 3), () {
      ToastUtils.showLoading = false;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        width: 180, // 固定宽度，居中浮层更像 Toast，而不是整条横幅
        shape: RoundedRectangleBorder(
          // 圆角胶囊样式
          borderRadius: BorderRadius.circular(40),
          // borderRadius: BorderRadiusGeometry.circular(40),
        ),
        behavior: SnackBarBehavior.floating, // 悬浮显示，不挤压页面布局
        duration: Duration(seconds: 3),      // 展示 3 秒（与解锁时间对齐）
        content: Text(
          msg ?? "加载成功", // msg 为空时兜底文案
          textAlign: TextAlign.center, // 文字居中
        ),
      ),
    );
  }
}