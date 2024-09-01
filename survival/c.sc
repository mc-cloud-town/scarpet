// 原出處: https://github.com/gnembon/scarpet/blob/master/programs/survival/cam.sc
// 修改: 猴貓 (https://github.com/a3510377)

// Default language
// zh_tw, en_us, zh_cn
DEFAULT_LANGUAGE = 'en_us';

__config() -> {
    'stay_loaded' -> 'true',
    'commands' -> {
        '' -> _() -> __check_type(player()),
        '<player_or_location>' -> _(value) -> (
            args = split(' ', value);
            len = length(args);
            m_p = player();

            if(len == 1, (
                p = player(value);
                if (p, (
                    if (m_p~'gamemode' != 'spectator', __to_spectator(m_p));
                    if (m_p == p, __tp_to_base_location(p), run('tp ' + p~'command_name'));
                ), display_title(m_p, 'actionbar', i18n(m_p, 'playerNotFound')))
            ), (
                // /c <x> <y> <z>
                // /c <x> <y> <z> in <dimension>
                if (len != 3 && len != 5, (
                    display_title(m_p, 'actionbar', i18n(m_p, 'invalidLocation'));
                    return();
                ));

                n_args = map(slice(args, 0, 3), number(_));
                if(!all(n_args, _ != null), (
                    display_title(m_p, 'actionbar', i18n(m_p, 'invalidLocation'));
                    return();
                ));

                dimension = args:4;
                if (dimension~system_info('world_dimensions') == null, dimension = m_p~'dimension');

                if (m_p~'gamemode' != 'spectator', __to_spectator(m_p));
                run('execute in '+dimension+' run tp ' + m_p~'command_name' + ' ' + join(' ', n_args));
            ))
        ),
    },
    'arguments' -> {
        'player_or_location' -> {
            'type' -> 'text',
            'suggester' -> _(args) -> (
                nameset = {'@r'};
                player_or_location = args:'player_or_location' || '';
                arg_loc = replace(player_or_location, ' +$', '');
                args_loc = split(' ', if(arg_loc == '', null, arg_loc));
                len = length(args_loc);
                // if(player_or_location && slice(player_or_location, -1) == ' ', len += 1);

                for(player('all'), nameset += _);
                if(len < 3, (
                    if (!len || number(args_loc:0) != null, (
                        for(pos(player()), (
                            if(_i > len - 1, (
                                if (_i, arg_loc += ' ');
                                arg_loc += str('%.2f', _);
                                nameset += arg_loc;
                            ));
                        ));
                    ));
                ), (
                    arg = args_loc:3;
                    if (len == 3 || len == 4, (
                        for(
                            system_info('world_dimensions'),
                            nameset += arg_loc+(if(arg == 'in', ' ', ' in '))+_
                        );
                    ));
                ));

                keys(nameset)
            ),
            'single' -> true,
        }
    },
};


global_languages = {
    'zh_tw' -> {
        'playerNotFound' -> format('r 找不到玩家'),
        'exitCameraMode' -> format('y 退出相機模式'),
        'enterCameraMode' -> format('y 進入相機模式'),
        'invalidLocation' -> format('rb 無效位置'),
        'dataLossWarning' -> format('rb 您的數據丟失將於該附近安全地方回復'),
        'noSafeLocation' -> format('rb 在 32 格範圍內未找到安全位置。')
    },
    'en_us' -> {
        'playerNotFound' -> format('r  Player not found'),
        'exitCameraMode' -> format('y Exit camera mode'),
        'enterCameraMode' -> format('y Enter camera mode'),
        'invalidLocation' -> format('rb Invalid location'),
        'dataLossWarning' -> format('rb Your lost data will be recovered in this nearby safe place'),
        'noSafeLocation' -> format('rb No safe location found within 32 bolcks.')
    },
    'zh_cn' -> {
        'playerNotFound' -> format('r 找不到玩家'),
        'exitCameraMode' -> format('y 退出相机模式'),
        'enterCameraMode' -> format('y 进入相机模式'),
        'invalidLocation' -> format('rb 无效位置'),
        'dataLossWarning' -> format('rb 您的数据丢失将于该附近安全地方回復'),
        'noSafeLocation' -> format('rb 在 32 格范围内未找到安全位置。')
    },
};

i18n(player, key) -> return(((global_languages:(player~'language')):key || (global_languages:DEFAULT_LANGUAGE):key) || key);

__tp_to_base_location(player) -> (
    config = __get_store_player_data(player);

    if (config,
        [x, y, z] = config:'pos';
        run('execute in '+config:'dimension'+' run tp @s '+x+' '+y+' '+z);
        for(l('yaw', 'pitch'), modify(player, _, config:_));
    );

    config
);

__restore_player_params(player) -> (
    config = __tp_to_base_location(player);

    if (config,(
        try (
            modify(player, 'motion', config:'motion');
            for (config:'effects', modify(player, 'effect', _:'name', _:'duration', _:'amplifier'));
        );

        display_title(player, 'actionbar', i18n(player, 'exitCameraMode'));
    ), (
        if (
            __safe_survival(player),
            display_title(player, 'actionbar', i18n(player, 'dataLossWarning')),
            return()
        );
    ));

    modify(player, 'gamemode', 'survival');
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
    modify(player, 'gamemode' , 'spectator')
);
