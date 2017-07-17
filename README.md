# VideoChat
This is chatroulette for android application.

When application start:
1.Cheking internet connection.
2.Connect to myne Heroku server (OpenTok Server SDKs with Node.js).
3.If all fine and session connecting fine, create stream
4.When another device connecting to session, it's created stream and looking for publisher streams, connected to them.
5.If 3-rd device connect to session he received that here is already 2 streams active.
6.Create new session and wait another device.
