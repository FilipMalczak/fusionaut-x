# FusionAuth-X

> X stands for "extended"

![Premature alpha](https://img.shields.io/badge/v-0.1.0-blueviolet.svg)
[![Build Status](https://travis-ci.com/FilipMalczak/fusionaut-x.svg?branch=master)](https://travis-ci.com/FilipMalczak/fusionaut-x)

> Make sure to read "FusionAuth licensing" section

## What is this?

> TL;DR - see **bold** down there 🡫

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

This repository (and it's 
[dockerhub counterpart](https://hub.docker.com/r/filipmalczak/fusionauth-x)) is a
 solution to that issue. **It is a base for a `filipmalczak/fusionauth-x` 
 Docker image that is based off a `fusionauth/fusionauth-app` one (with the same tag).**
 
**This layer does not change anything - it just adds a FA plugin to the image;
 that plugin analyses some env vars and performs these manual actions automatically.**
 
 ## How to use
 
 > I'll write this down soon. It evolves too fast at this point.
 
 ## FusionAuth licensing
 
 > This is really important. Focus.
 
 Consider that a legal notice, that using anything that the 
 [attached licence](./LICENSE) refers to as "Work" or "Derivative Works" is
 equivalent with agreeing to license terms that would otherwise occur in the
 manual process of initializing FusionAuth instance.
 
 # Developer notes
 
 If you want to develop, go with:
 
 - (fork and) checkout the repo
 - make changes to the code
 - make changes to env vars in [docker-compose.yml](./docker-compose.yml)
 - bring it up with `user@cloned_repo$ docker-compose up`
 - check out the instance at [localhost:9011](http://localhost:9011)
 
 > TODO
 > - fill the README
 > - manage versions in build, travis and README badge (probably somewhere else)
 > - refactor BootstrapImpl to two steps
 
 > FIXME
 > - registering an admin is not working. 
 > - registering an admin with one call with `userMapper.create` with a `UserRegistration` did not created the user registration in db.
 > - registering and admin in two separate calls (`User` and `UserRegistration`) is creating both data in db.
 > - the differeneces between a user registered by bootstrap and a user registered manually with the setup wizard are: 1. there is no relation between the admin role and the userRegistration. (table: `user_registrations_application_roles`). 2. the user created manually is present in the table `identities`. 3. the user created by bootstrap is not visible in FA panel.
 > - i've tried to add the relation role-user_registration manually in db - did not help.
 > - i didnt find any mapper to map a `UserRegistration` to a `Role`
 
 > Questions
 > - are tou sure that first and last name is not needed ? when filling the form manually the field is required