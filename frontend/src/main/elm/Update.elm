module Update exposing (..)

import Msgs exposing (Msg(..))
import Models exposing (Model)
import Net exposing (..)

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    MorePlease ->
      (model, getRandomGif model.topic)

    NewGif (Ok newUrl) ->
      ({ model | topic = model.topic, gifUrl = newUrl }, Cmd.none)

    NewGif (Err _) ->
      (model, Cmd.none)

    Go section ->
      ({ model | section = section }, Cmd.none)
