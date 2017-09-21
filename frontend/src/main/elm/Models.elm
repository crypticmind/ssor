module Models exposing (..)

type Section = Home | UserAdmin

type alias Model =
  { section: Section
  , topic : String
  , gifUrl : String
  }
