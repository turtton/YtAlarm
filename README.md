<p align="center"><img src="docs/logo/logo-no-background.png"></p>
<h4 align="center"><b>A simple alarm app using youtubedl-android</b></h4>

<p align="center">
<a href="https://github.com/turtton/YtAlarm/actions/workflows/check_code.yml">
    <img src="https://img.shields.io/github/checks-status/turtton/YtAlarm/main?style=flat-square">
<a href="https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=turtton/YtAlarm&amp;utm_campaign=Badge_Grade">
    <img alt="Codacy branch grade" src="https://img.shields.io/codacy/grade/5f8c410c677a4172a5641242bf40d6c4/main?style=flat-square">
</a>
<a href="https://codecov.io/gh/turtton/YtAlarm"> 
    <img alt="Codecov branch" src="https://img.shields.io/codecov/c/github/turtton/YtAlarm/main?style=flat-square&token=KBB10HH0TL">
</a>
<a href="https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm?ref=badge_shield" alt="FOSSA Status">
    <img src="https://app.fossa.com/api/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm.svg?type=shield"/>
</a>
</p>

------

Other languages: [日本語](docs/readme/README_ja.md)

**Attention: Do not put this application or any fork of it into google play store. It violates their teams and conditions.**

YtAlarm is a simple alarm application using [youtubedl-android](https://github.com/yausername/youtubedl-android). It makes be possible to wake up with a video/music which you like.  
This application does not use Google's developer services or service-dpendent APIs such as YoutubeAPI, so it will work without problems on any Android device that meets the supported Api level(probably).  
Also, you do not need an account for each service to use this application.

# Screenshots

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/alarm.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/alarm.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/alarms.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/alarms.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/alarmSettings.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/alarmSettings.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/playlist.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/playlist.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videos-origin.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/videos-origin.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videos-playlist.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/videos-playlist.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/allvideos.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/allvideos.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/drawer.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/drawer.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videoplayer.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/videoplayer.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/aboutpage.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/aboutpage.png)

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

<table>
    <tr>
        <td><img src="https://liberapay.com/assets/liberapay/logo-v2_black-on-yellow.svg?etag=.yjV53S_Yb2wp7l1bfBotLA~~"></td>
        <td><p align="center"><a href="https://liberapay.com/turtton/donate"><img src="docs/qr/qr_liberapay.png" width="33%" height="33%"></a></p></td>
        <td><a href="https://liberapay.com/turtton/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" width="50%" height="50%"></a></td>
    </tr>
    <tr>
        <td><p align="center"><img src="https://bitcoin.org/img/icons/logotop.svg"></p></td>
        <td><p align="center"><img src="https://bitflyer.com/ex/qr?text=3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t" width="33%" height="33%"></p></td>
        <td>3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t  </td>
    </tr>
</table>


# License

YtAlarm licensed under [GNU General Public License v3.0](https://github.com/turtton/YtAlarm/blob/HEAD/LICENSE)

Other libraries/tools licenses used by this project can be seen below.

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm.svg?type=large)](https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm?ref=badge_large)
