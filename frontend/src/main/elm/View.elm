module View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Models exposing (..)
import Msgs exposing (..)

view : Model -> Html Msg
view model =
  div []
    [ h1 [] [ text "App" ]
    , div []
        [ button [ onClick <| Go Home ] [ text "Home" ]
        , button [ onClick <| Go UserAdmin ] [ text "Users" ]
        ]
    , case model.section of
        Home -> homeView model
        UserAdmin -> userAdminView model
    ]

homeView : Model -> Html Msg
homeView model =
  div []
    [ h2 [] [ text <| "Home. Topic: " ++ model.topic ]
    , button [ onClick MorePlease ] [ text "More Please!" ]
    , br [] []
    , img [ src model.gifUrl ] []
    ]

userAdminView : Model -> Html Msg
userAdminView model =
  div []
    [ h2 [] [ text "Users" ]
    ]
