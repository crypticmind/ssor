
import Html exposing (..)
import Models exposing (..)
import Msgs exposing (..)
import Net exposing (..)
import Update exposing (..)
import Subs exposing (..)
import View exposing (..)

main =
  Html.program
    { init = init "cars"
    , view = view
    , update = update
    , subscriptions = subscriptions
    }

init : String -> (Model, Cmd Msg)
init topic =
  ( Model Home topic "waiting.gif"
  , getRandomGif topic
  )
