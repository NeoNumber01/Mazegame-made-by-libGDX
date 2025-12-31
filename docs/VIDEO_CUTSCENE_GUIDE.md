# gdx-video 过场视频资源规范

## 1. 资源路径结构

```
assets/
├── startstory.mp4          # 开场剧情视频
├── spaceshipboard.mp4      # 出口触发的飞船过场视频
└── (其他游戏资源...)
```

## 2. 编码建议

### 推荐格式优先级

| 平台 | 推荐格式 | 原因 |
|------|----------|------|
| Desktop (Windows/Mac/Linux) | WebM (VP8/VP9 + Vorbis) 或 MP4 (H.264 + AAC) | gdx-video-lwjgl3 支持良好 |
| Android | MP4 (H.264 Baseline + AAC) | 硬件解码支持最广泛 |
| iOS | MP4 (H.264 + AAC) | 原生支持 |
| Web/HTML5 | WebM (VP8 + Vorbis) | 浏览器兼容性最佳 |

### 跨平台最佳选择
- **如果只针对 Desktop**: WebM 或 MP4 都可以
- **如果需要 Android 兼容**: 优先使用 **MP4 (H.264 Baseline Profile + AAC)**
- **如果需要 Web 兼容**: 优先使用 **WebM (VP8 + Vorbis)**

## 3. 分辨率/帧率/码率建议

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| 分辨率 | 1280x720 (720p) | 平衡画质与性能 |
| 帧率 | 24-30 fps | 避免高帧率导致解码压力 |
| 视频码率 | 2-4 Mbps | 移动设备友好 |
| 音频码率 | 128-192 kbps | 足够的音质 |
| 音频采样率 | 44100 Hz | 标准采样率 |

### 移动设备特别注意
- Android 低端设备建议 **854x480 (480p) @ 24fps**
- 避免使用 B-frames（某些设备不支持）
- H.264 使用 **Baseline Profile** 而非 Main/High Profile

## 4. 关键注意事项

### ⚠️ 重要：VideoPlayer 必须正确 dispose

```java
// 错误做法 ❌
videoPlayer.play(file1);
videoPlayer.play(file2);  // 某些设备会失败！

// 正确做法 ✅
videoPlayer.play(file1);
// ... 播放完成后
videoPlayer.dispose();
videoPlayer = null;

// 加载下一段
videoPlayer = VideoPlayerCreator.createVideoPlayer();
videoPlayer.play(file2);
```

### 文件存在性检查
```java
FileHandle videoFile = Gdx.files.internal(videoPath);
if (!videoFile.exists()) {
    Gdx.app.error("Video", "File not found: " + videoPath);
    // fallback to skip cutscene
    return;
}
```

## 5. FFmpeg 转换命令

### 转换为 MP4 (H.264 + AAC) - 推荐用于 Android

```bash
# 基础转换 (720p, 30fps, 高质量)
ffmpeg -i input.mp4 -c:v libx264 -profile:v baseline -level 3.1 \
       -pix_fmt yuv420p -vf "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2" \
       -r 30 -b:v 3M -maxrate 4M -bufsize 6M \
       -c:a aac -b:a 192k -ar 44100 -ac 2 \
       -movflags +faststart \
       output.mp4

# 移动设备友好版 (480p, 24fps, 低码率)
ffmpeg -i input.mp4 -c:v libx264 -profile:v baseline -level 3.0 \
       -pix_fmt yuv420p -vf "scale=854:480:force_original_aspect_ratio=decrease,pad=854:480:(ow-iw)/2:(oh-ih)/2" \
       -r 24 -b:v 1.5M -maxrate 2M -bufsize 3M \
       -c:a aac -b:a 128k -ar 44100 -ac 2 \
       -movflags +faststart \
       output_mobile.mp4
```

### 转换为 WebM (VP8 + Vorbis) - 推荐用于 Desktop/Web

```bash
# 高质量 WebM (720p, 30fps)
ffmpeg -i input.mp4 -c:v libvpx -quality good -cpu-used 0 \
       -vf "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2" \
       -r 30 -b:v 3M -maxrate 4M \
       -c:a libvorbis -b:a 192k -ar 44100 -ac 2 \
       output.webm

# VP9 (更好的压缩率，但编码较慢)
ffmpeg -i input.mp4 -c:v libvpx-vp9 -quality good -cpu-used 2 \
       -vf "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2" \
       -r 30 -b:v 2M -maxrate 3M \
       -c:a libopus -b:a 128k -ar 48000 -ac 2 \
       output_vp9.webm
```

### 快速转换命令 (针对你的项目)

```bash
# 转换 startstory 视频
ffmpeg -i startstory_source.mp4 -c:v libx264 -profile:v baseline -level 3.1 \
       -pix_fmt yuv420p -vf "scale=1280:720" -r 30 -b:v 3M \
       -c:a aac -b:a 192k -movflags +faststart \
       startstory.mp4

# 转换 spaceshipboard 视频
ffmpeg -i spaceshipboard_source.mp4 -c:v libx264 -profile:v baseline -level 3.1 \
       -pix_fmt yuv420p -vf "scale=1280:720" -r 30 -b:v 3M \
       -c:a aac -b:a 192k -movflags +faststart \
       spaceshipboard.mp4
```

## 6. 测试清单

- [ ] Desktop 上播放流畅
- [ ] 视频结束后正确触发 onFinished 回调
- [ ] ESC/SPACE 跳过功能正常
- [ ] 切换到下一个 Screen 后无内存泄漏
- [ ] 返回菜单再开始游戏，视频不重复播放（或按设计重复）
- [ ] Android 设备上测试（如果适用）

## 7. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 视频不播放/黑屏 | 编码格式不支持 | 使用 H.264 Baseline Profile |
| 播放卡顿 | 码率过高/分辨率过高 | 降低到 720p/2Mbps |
| 第二段视频无法加载 | VideoPlayer 未 dispose | 确保每段播完后 dispose |
| Android 崩溃 | 使用了不支持的 Profile | 使用 Baseline Profile |
| 音频不同步 | 编码问题 | 重新用 ffmpeg 转码 |

