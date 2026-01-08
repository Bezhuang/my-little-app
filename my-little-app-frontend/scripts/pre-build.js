// 构建前脚本 - 自动注入敏感配置
// 运行此脚本: node scripts/pre-build.js

const fs = require('fs')
const path = require('path')

// 读取 constant.js
const constantPath = path.join(__dirname, '../src/constant.js')
const manifestPath = path.join(__dirname, '../src/manifest.json')

// 从 constant.js 提取 appid
const constantContent = fs.readFileSync(constantPath, 'utf-8')
const appidMatch = constantContent.match(/WEIXIN_APPID\s*=\s*['"]([^'"]+)['"]/)

if (!appidMatch) {
  console.error('错误: 无法在 constant.js 中找到 WEIXIN_APPID')
  process.exit(1)
}

const weixinAppid = appidMatch[1]

// 更新 manifest.json
let manifest = fs.readFileSync(manifestPath, 'utf-8')
manifest = manifest.replace(/("appid"\s*:\s*)"__WEIXIN_APPID__"/, `$1"${weixinAppid}"`)

fs.writeFileSync(manifestPath, manifest)
console.log(`已注入微信小程序 AppID: ${weixinAppid}`)
