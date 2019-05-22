#FusionAuth-x

> X stands for "extended"

![Premature alpha](https://img.shields.io/badge/v-0.1.0-blueviolet.svg)

> Make sure to read "FusionAuth licensing" section

## What is this?

So, don't get me wrong. FusionAuth is not my product. It's a wonderful product,
 but all the praises belong to the [creators](https://fusionauth.io).

Lately I've been in love with containerization and found FA really useful.
 Unfortunately, there was one problem - on first startup of the whole
 docker-compose project, I would need to manually expose FA to a machine where
 a I would be able to open a browser; I would then
 need to register an admin account, set up an API key, put it somewhere where
 other containers could pick it up (to create clients), then there would be
 a smackload of restarting, scripting and black magic and finally I could
 (but not always would) end up with the whole system in the running state.
 
> It sounds like a flattery, but once that part was over, it all went
smoothly. I wouldn't make an OS project for commercial thing that I would
not personally recommend.

This repository (and it's dockerhub counterpart) is a solution to that issue. 
 It is a base for a `filipmalczak/fusionauth-x` Docker image that is based off
 a `fusionauth/fusionauth-app` one (with the same tag).
 
**This layer does not change anything - it just adds a FA plugin to the image;
 that plugin analyses some env vars and performs this manual actions automatically.**
 
 ## How to configure
 
 > I'll write this down soon. It evolves too fast at this point.
 
 ## FusionAuth licensing
 
 > This is really important. Focus.
 
 Consider that a legal notice, that using anything that the 
 [attached licence](./LICENSE) refers to "Work" or "Derivative Works" is
 equivalent with agreeing to license terms that would otherwise occur in the
 manual process of initializing FusionAuth instance.
 