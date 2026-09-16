import 'package:get/get.dart';
import 'package:view/viewmodels/user.dart';

// 需要共享的对象 要有一些共享的属性 属性需要响应式更新
class UserController extends GetxController {
  var user = UserInfo.fromJSON({}).obs; // user对象被监听了
  // 想要取值的话 需要 user.value
  updateUserInfo(UserInfo newUser) {
    user.value = newUser;
  }
}
