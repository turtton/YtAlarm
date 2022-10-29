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
<a href="https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm?ref=badge_shield" alt="FOSSA Status"><img src="https://app.fossa.com/api/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm.svg?type=shield"/></a>
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
- ~~download~~(In progress [#140](https://github.com/turtton/YtAlarm/issues/140))

### Media Management

- Playlist Import

# Supported Services

Internally, the services mentioned [here](https://github.com/yt-dlp/yt-dlp/tree/master/yt_dlp/extractor) will work since this application uses [yt-dlp](https://github.com/yt-dlp/yt-dlp), but some services only support  download mode.

[Available services](docs/AVAILABLE_SERVICES.md)

# Contribution

Whether bug reporting, feature requests, translations, code changes, help is always welcome!!

See [Contributing](.github/CONTRIBUTING.md) for more information.

# Donate

If you like YtAlarm, you're welcome to send a donation.

| ![](https://liberapay.com/assets/liberapay/logo-v2_black-on-yellow.svg?etag=.yjV53S_Yb2wp7l1bfBotLA~~) | <img src="docs/qr/qr_liberapay.png" alt="image-20221029181632991" style="zoom: 33%;" /> | <a href="https://liberapay.com/turtton/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" style="zoom:200%;"   ></a> |
| :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
|        ![](https://bitcoin.org/img/icons/logotop.svg)        | ![](https://bitflyer.com/ex/qr?text=3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t) |              3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t              |



# License

YtAlarm licensed under [GNU General Public License v3.0](https://github.com/turtton/YtAlarm/blob/HEAD/LICENSE)

Other libraries/tools licenses used by this project can be seen below.

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm.svg?type=large)](https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm?ref=badge_large)
