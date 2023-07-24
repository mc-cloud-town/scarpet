// 原出處: https://github.com/gnembon/scarpet/blob/master/programs/survival/cam.sc
// 修改: 猴貓 (https://github.com/a3510377)

__config() -> {
    'stay_loaded' -> 'true',
    'commands' -> {
        '' -> _() -> __check_type(player()),
        '<tp_player>' -> _(tp_player) -> (
            p = player(tp_player);
            if (player() == p, return(run('tellraw @s "'+i18n(player(), 'cannotWatchSelf')+'"')));
            if (p, (
                if (!__get_store_player_data(player()), __to_spectator(player()));
                run('tp ' + p~'command_name')
            ), run('tellraw @s "'+i18n(player(), 'playerNotFound')+'"'))
        )
    },
    'arguments' -> {'tp_player' -> {'type' -> 'players', 'single' -> true}},
};


global_languages = {
    'zh_tw' -> {
        'cannotWatchSelf' -> format('r 您不能旁觀自己'),
        'playerNotFound' -> format('r 找不到玩家'),
        'exitCameraMode' -> format('y 退出相機模式'),
        'enterCameraMode' -> format('y 進入相機模式，以將您位置儲存'),
        'dataLossWarning' -> format('rb 您的數據丟失將於該附近安全地方回復'),
        'noSafeLocation' -> format('rb 在 32 格範圍內未找到安全位置。')
    },
    'en_us' -> {
        'cannotWatchSelf' -> format('r You cannot watch yourself'),
        'playerNotFound' -> format('r  Player not found'),
        'exitCameraMode' -> format('y Exit camera mode'),
        'enterCameraMode' -> format('y Enter camera mode, to save your location'),
        'dataLossWarning' -> format('rb Your lost data will be recovered in this nearby safe place'),
        'noSafeLocation' -> format('rb No safe location found within 32 bolcks.')
    },
    'zh_cn' -> {
        'cannotWatchSelf' -> format('r 您不能旁观自己'),
        'playerNotFound' -> format('r 找不到玩家'),
        'exitCameraMode' -> format('y 退出相机模式'),
        'enterCameraMode' -> format('y 进入相机模式，以将您位置储存'),
        'dataLossWarning' -> format('rb 您的数据丢失将于该附近安全地方回復'),
        'noSafeLocation' -> format('rb 在 32 格范围内未找到安全位置。')
    },
};

i18n(player, key) -> return(((global_languages:(player~'language')):key || (global_languages:'en_us'):key) || key);

__restore_player_params(player) -> (
    config = __get_store_player_data(player);

    if (config, 
        run('execute in ' + config:'dimension' + ' run tp @s ~ ~ ~');

        try (
            modify(player, 'location', [
                ...config:'pos',
                config:'yaw',
                config:'pitch'
            ]);
            modify(player, 'motion', config:'motion');
            for (config:'effects', modify(player, 'effect', _:'name', _:'duration', _:'amplifier'));
        );
        display_title(player, 'actionbar', i18n(player, 'exitCameraMode')), (
            if (
                __safe_survival(player),
                display_title(player, 'actionbar', i18n(player, 'dataLossWarning')),
                return()
            );
        )
    );

    modify(player, 'gamemode', config:'gamemode');
    __remove_player_config(player);
);

__safe_survival(player) -> (
    yposes = l();
    l(x, y, z) = pos(player);
    for(range(32), yposes += y + _; yposes += y - _);
    for(yposes,
        scan(x, _, z, 32, 0, 32,
            up = pos_offset(_, 'up');
            down = pos_offset(_, 'down');
            if(air(_) && air(up) && suffocates(down) && !flammable(down),
                modify(player, 'pos', pos(_)+l(0.5,0.2,0.5));
                return(true);
            )
        )
    );
    display_title(player, 'actionbar', i18n(player, 'noSafeLocation'));
    false
);

__to_spectator(player) -> (
    __store_player_data(player);
    modify(player, 'effect');
    modify(player, 'gamemode', 'spectator');

    display_title(player, 'actionbar', i18n(player, 'enterCameraMode'));
);

__check_type(player) -> (
    if (
        player~'gamemode' == 'spectator', __restore_player_params(player),
        __to_spectator(player)
    );
);

__remove_player_config(player) -> (
   tag = load_app_data();
   delete(tag:(player~'name'));
   store_app_data(tag);
);

__store_player_data(player) -> (
    tag = nbt('{}');

    for(pos(player), put(tag:'Position', str('%.6fd', _), _i)); 
    for(player~'motion', put(tag:'Motion', str('%.6fd', _), _i)); 

    tag:'Yaw' = str('%.6f', player~'yaw');
    tag:'Pitch' = str('%.6f', player~'pitch');
    tag:'Dimension' = player~'dimension';
    tag:'gamemode' = player~'gamemode';

    for (player~'effect',
        l(name, amplifier, duration) = _;
        etag = nbt('{}');
        etag:'Name' = name;
        etag:'Amplifier' = amplifier;
        etag:'Duration' = duration;
        put(tag:'Effects', etag, _i);
    );

    apptag = load_app_data();
    if (!apptag, apptag = nbt('{}'));
    apptag:(player~'name') = tag;

    store_app_data(apptag);
);

__get_store_player_data(player) -> (
    tag = load_app_data();
    if(!tag, return (null));

    player_tag = tag:(player~'name');
    if (!player_tag, return(null));

    config = m();
    config:'pos' = player_tag:'Position.[]';
    config:'motion' = player_tag:'Motion.[]';
    config:'yaw' = player_tag:'Yaw';
    config:'pitch' = player_tag:'Pitch';
    config:'dimension' = player_tag:'Dimension';
    config:'effects' = l();
    config:'gamemode' = player_tag:'gamemode';
    effects_tags = player_tag:'Effects.[]';

    if (effects_tags, for(effects_tags, etag = _;
        effect = m();
        effect:'name' = etag:'Name';
        effect:'amplifier' = etag:'Amplifier';
        effect:'duration' = etag:'Duration';
        config:'effects' += effect;
    ));

    config
);

__on_player_connects(player) -> if(
    __get_store_player_data(player),
    __remove_player_config(player)
);
