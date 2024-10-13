# Android Permission Dialog for Jetpack Compose

`PermissionDialog` composable that shows Android permission dialog, calling the requested callbacks
when the permission has been granted or denied, and allowing to perform a custom action when the
permission has been permanently denied by the user or the Android system (i.e. when the user has 
selected the 'Never ask me again' option on older Android versions, or denied the permission more 
than one time on newer Android versions).
