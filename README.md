# Android Spotify SDK Application

## Intro
Spotify has a Software Development Kit which allows developers to utilize it to gain access to Spotify's database of songs, artists, playlists, etc... as well as control playback features. We (group of three St. Lawrence University students) have decided to implement our own version of Spotify that is much more simplistic. In order to utilize this app, the user must add their SHA1 key for the Android S2 tablet onto the developer database website that Spotify has (https://developer.spotify.com/dashboard/applications). Spotify's SDK gives us access to the songs but since there is not much work done for the Android Version in order to obtain information about playlists and authors. In order to get around this, we used the Kaees Web API Wrapper library (https://github.com/kaaes/spotify-web-api-android) to make web requests to Spotify's Web Version so that we can obtain the information. 

## Spotify SDK
In order to utilizing the playback features smoothly without having to reimplement everytime, we designed a RemoteService class which uses Object Oriented Principles. After implementing all the features needed through methods (play song, pause song, next song, previous song, etc...), we initialized the class as an Android Service. This allowed us to reconnect the service in every activity with only a couple lines of code rather than having to set it all up again. The other thing we had the RemoteService class doing is sending the song information through Intents everytime a new activity is created. Since it is ran as a Service, it is always running in the background so it never loses the information of the current song playing. As a result, this allowed us to update all the fields containing the playback information in real time as the activity gets created. 

## Kaees Web API Wrapper library
Since Kaees allows us to get further information and is what we had to utilize in order to search for any information. In order to utilize it, you must first tell it what scopes you want to use (https://developer.spotify.com/documentation/general/guides/scopes/) and authorize it with a token. However, the token expires every hour so whenever it is gone we need to make sure we reauthroize our user or else they will not be able to make anymore requests. 

## Home Activity
When the user opens up the app, they will see three tabs (Search, Library, Playing) at the bottom, a play/pause button and floating text next to it which is our Home screen. A message displays at the top giving credit to Spotify since all our data comes from there. If there is a song already playing, the user can pause or play it by tapping on the button. The user can also skip to the next song by swiping right or go to the previous song by swipping left. Let us now say that the user taps on the Search button at the bottom. 

## Search Activity
At the very top there is a blank rectangle in which the user can type any text into with a Search button next to it. Once the user types in text (search with no text is not allowed) and hits search, a Search request is sent out using the Kaees library and the search object is saved. The search object is then displayed through a vertical ScrollView which will appear with all the songs that matched the search query and the user can tap on any of them to play the song.  

## Library Activity
When the user opens the Library Activity, a request is sent out to obtain all the current user's playlists. All the playlists are then displayed in a horizontal ScrollView in which the user can look through. Once one of those playlists is clicked, a vertical ScrollView will appear with all the songs within that playlist (similar to the Search Activity).

## Playing Activity
This activity shows the song that is currently playing. Within the tab, the user sees a bigger interface about the song details. It shows the album cover for the song, the user can skip or go previous, and the user can also change the volume with a slide bar. If the user taps on the album cover, an option will pop up to "Like" the song. After the user successfully likes the song, they can go back into their Library Activity and see that the song shows up in the Liked playlist. 
