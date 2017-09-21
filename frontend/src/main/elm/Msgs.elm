module Msgs exposing (..)

import Http
import Models exposing (..)

type Msg
  = MorePlease
  | NewGif (Result Http.Error String)
  | Go Section
