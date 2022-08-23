// 製作: 猴子 (https://github.com/a3510377)

__config()->{
    'scope'->'global',
    'stay_loaded'->true,
};

__on_start() -> (
    team_add('carpet_bot_team');
    team_property('carpet_bot_team', 'displayName', 'bot_team');
    team_property('carpet_bot_team', 'color', 'gray');
    team_property('carpet_bot_team', 'prefix', '[bot] ');
    // team_property('carpet_bot_team', 'collisionRule', 'never');

    for (player('all'), _edit_player(_))
);

__on_close() -> team_remove('carpet_bot_team');

__on_player_connects(player) -> _edit_player(player);

_edit_player(player) ->  if (
    player~'player_type' == 'fake',
    team_add('carpet_bot_team', player),
    if (player~'team' == 'carpet_bot_team', team_leave(player))
);
