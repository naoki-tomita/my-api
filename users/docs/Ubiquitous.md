# ユーザー管理モジュール

## ユーザー

アクターであるユーザーをさす

## ログインユーザー

ログイン済みのユーザーのこと

## 匿名ユーザー

ログインしていないユーザーのこと

## ログイン

ログインリクエストで一意に定まるユーザーを特定し、セッションを発行すること

### ログインリクエスト

IDとパスワードの二つの文字列を持つ

## ログアウト

発行したセッションを破棄すること

## セッション

セッションは、ログイン時に発行される
セッションを使うと、ログインユーザーを特定できる

## セッションID

セッションはセッションIDを元に特定できる
ユーザーは、セッションIDを示すことで、特定のログインユーザーであることを証明することができる
