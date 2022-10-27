<p align="center"><img src="docs/logo/logo-no-background.png"></p>
<h4 align="center"><b>A simple alarm app using youtubedl-android</b></h4>

<p align="center">
<a href="https://github.com/turtton/YtAlarm/actions/workflows/check_code.yml">
    <img src="https://github.com/turtton/YtAlarm/actions/workflows/check_code.yml/badge.svg">
</a>
<a href="https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=turtton/YtAlarm&amp;utm_campaign=Badge_Grade">
    <img src="https://app.codacy.com/project/badge/Grade/5f8c410c677a4172a5641242bf40d6c4">
</a>
<a href="https://codecov.io/gh/turtton/YtAlarm"> 
    <img src="https://codecov.io/gh/turtton/YtAlarm/branch/main/graph/badge.svg?token=KBB10HH0TL"/> 
</a>
</p>

------

Other languages: [日本語](docs/readme/README_ja.md)

**Attention: Do not put this application or any fork of it into google play store. It violates their teams and conditions.**

YtAlarm is a simple alarm application using [youtubedl-android](https://github.com/yausername/youtubedl-android). It makes be possible to wake up with a video/music which you like.  
This application does not use Google's developer services or service-dpendent APIs such as YoutubeAPI, so it will work without problems on any Android device that meets the supported Api level(probably).  
Also, you do not need an account for each service to use this application.

# Feature

### Alarm

- Repeat(Every day, Day of week, Date)
- Loop
- Volume Setting
- Snooze

### Media player

- streaming
- ~~download~~(In progress [#65](https://github.com/turtton/YtAlarm/issues/65))

### Media Management

- Playlist Import

# Supported Services

Internally, the services mentioned [here](https://github.com/yt-dlp/yt-dlp/tree/master/yt_dlp/extractor) will work since this application uses [yt-dlp](https://github.com/yt-dlp/yt-dlp), but some services only support  download mode.

[Available services](docs/AVAILABLE_SERVICES.md)

# Contribution

Whether bug reporting, feature requests, translations, code changes, help is always welcome!!

See [Contributing](.github/CONTRIBUTING.md) for more information.

