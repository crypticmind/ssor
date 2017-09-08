import Html exposing (beginnerProgram, div, button, text, input)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)


main =
  beginnerProgram { model = model, view = view, update = update }


view model =
  div []
    [ div [] [ text <| String.toUpper model.content ]
    , input [ placeholder "Input text", onInput Change ] []
    ]

type alias Model =
    { content : String
    }

model : Model
model =
    { content = ""
    }

type Msg = Change String


update msg model =
  case msg of
    Change text ->
      { model | content = text }
